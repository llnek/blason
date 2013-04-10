<#if scope.col = true >
  def ${scope.setter}(p:${scope.param_type}) = writeData(${scope.colname}, Option(p))

  def ${scope.getter}() = ${scope.reader}(${scope.colname})

</#if>
<#if scope.o2o = true >
  def ${scope.getter}[T <: ${scope.rhstype}](db:SQLProc, rhs:Class[T], cache:Boolean = false )(implicit m: Manifest[T]): Option[T] = {
    if (cache) getRef(${scope.colname}) match {
      case x:T => return Option(x)
      case _ =>
    }
    val rc= db.getO2O(this, rhs, ${scope.colname} )
    setRef( ${scope.colname}, rc.getOrElse(null) )
    rc
  }

  def ${scope.setter}[T <: ${scope.rhstype}](p:T ) = setO2O( p, ${scope.colname} )

</#if>
<#if scope.o2m = true >
  def ${scope.getter}(db:SQLProc, cache:Boolean = false): Seq[${scope.rhstype}] = {
    if (cache) getSeq("${scope.getter}") match {
      case x:Seq[_] if x.size > 0 => return x.asInstanceOf[Seq[${scope.rhstype}]]
      case _ =>
    }
    val rc= db.getO2M(this, classOf[${scope.rhstype}], ${scope.colname} )
    setRef("${scope.getter}", rc.toList )
    rc
  }

  def ${scope.adder}(d:${scope.rhstype} ) = linkO2M(d, ${scope.colname} )

  def ${scope.delone}(d:${scope.rhstype} ) = unlinkO2M(d, ${scope.colname})

  def ${scope.delall}(db:SQLProc) = db.purgeO2M(this, classOf[${scope.rhstype}], ${scope.colname} )

</#if>
<#if scope.m2m = true >
  def ${scope.getter}( db:SQLProc) = db.getM2M(this, classOf[${scope.rhstype}] )

  def ${scope.delone}(db:SQLProc, e:${scope.rhstype} ) = db.unlinkM2M(this, e)

  def ${scope.delall}(db:SQLProc) = db.purgeM2M(this, classOf[${scope.rhstype}] )

  def ${scope.adder}(db:SQLProc, e:${scope.rhstype}) = db.linkM2M(this, e)

</#if>
