package scala.meta.tests.metacp

import scala.collection.mutable
import scala.meta.internal.semanticdb.Locator
import scala.meta.internal.{semanticdb => s}
import scala.meta.io.Classpath

object MetacpOps {

  /** Returns all symbols that have an associated SymbolInformation in the classpath */
  def collectAllGlobalSymbols(classpath: Classpath): collection.Set[String] = {
    val allSymbols = mutable.Set.empty[String]
    Locator(classpath.entries.map(_.toNIO)) { (_, docs) =>
      for {
        doc <- docs.documents
        sym <- doc.symbols
      } {
        allSymbols += sym.symbol
      }
    }
    allSymbols
  }

  /** Returns all symbols that are referenced from the signature of info */
  def collectAllSymbolReferences(info: s.SymbolInformation): List[String] = {
    val references = List.newBuilder[String]
    val isHardlink = mutable.Set.empty[String]
    def visitSymbol(symbol: String): Unit = {
      if (!isHardlink(symbol)) {
        references += symbol
      }
    }
    def visitScope(scope: Option[s.Scope])(thunk: () => Unit): Unit = {
      var toEnter: Seq[String] = Nil
      scope.foreach { s =>
        toEnter = s.hardlinks.map(_.symbol)
        isHardlink ++= toEnter
        s.symlinks.foreach(visitSymbol)
      }
      thunk()
      isHardlink --= toEnter
    }
    def visitType(tpe: s.Type): Unit = tpe match {
      case s.TypeRef(prefix, symbol, typeArguments, _) =>
        visitType(prefix)
        visitSymbol(symbol)
        typeArguments.foreach(visitType)
      case s.SingleType(prefix, symbol, _) =>
        visitType(prefix)
        visitSymbol(symbol)
      case s.ThisType(symbol, _) =>
        visitSymbol(symbol)
      case s.SuperType(prefix, symbol, _) =>
        visitType(prefix)
        visitSymbol(symbol)
      case s.ConstantType(_, _) =>
        ()
      case s.IntersectionType(types, _) =>
        types.foreach(visitType)
      case s.UnionType(types, _) =>
        types.foreach(visitType)
      case s.WithType(types, _) =>
        types.foreach(visitType)
      case s.StructuralType(tpe, declarations, _) =>
        visitScope(declarations) { () => visitType(tpe) }
      case s.AnnotatedType(annotations, tpe, _) =>
        annotations.foreach(annot => visitType(annot.tpe))
        visitType(tpe)
      case s.ExistentialType(tpe, declarations, _) =>
        visitScope(declarations) { () => visitType(tpe) }
      case s.UniversalType(typeParameters, tpe, _) =>
        visitScope(typeParameters) { () => visitType(tpe) }
      case s.ByNameType(tpe, _) =>
        visitType(tpe)
      case s.RepeatedType(tpe, _) =>
        visitType(tpe)
      case s.NoType =>
    }
    def visitSignature(signature: s.Signature): Unit = signature match {
      case s.ClassSignature(typeParameters, parents, self, declarations, _) =>
        visitScope(typeParameters) { () =>
          visitScope(declarations) { () =>
            parents.foreach(visitType)
            visitType(self)
          }
        }
      case s.MethodSignature(typeParameters, parameterLists, returnType, _) =>
        parameterLists.foreach(s => visitScope(Some(s))(() => ()))
        visitScope(typeParameters) { () => visitType(returnType) }
      case s.TypeSignature(typeParameters, lowerBound, upperBound, _) =>
        visitScope(typeParameters) { () =>
          visitType(lowerBound)
          visitType(upperBound)
        }
      case s.ValueSignature(tpe, _) =>
        visitType(tpe)
      case s.NoSignature =>
        ()
    }
    visitSignature(info.signature)
    references.result()
  }

  /** Returns all symbols are referenced in the classpath but have no associated SymbolInformation */
  def collectReferencedToUndefinedSymbols(classpath: Classpath): Iterable[String] = {
    val isPersistedGlobalSymbol = collectAllGlobalSymbols(classpath)
    val errors = mutable.Set.empty[String]
    Locator(classpath.entries.map(_.toNIO)) { (path, docs) =>
      for {
        doc <- docs.documents
        sym <- doc.symbols
      } {
        val references = collectAllSymbolReferences(sym)
        errors ++= references.filterNot(isPersistedGlobalSymbol)
      }
    }
    errors
  }

}
