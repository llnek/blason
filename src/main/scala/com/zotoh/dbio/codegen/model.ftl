<#if scope.col = true >
  def ${scope.setter}(p:${scope.param_type}) = writeData(${scope.colname}, Option(p))

  def ${scope.getter}() = ${scope.reader}(${scope.colname})

</#if>
<#if scope.o2o = true >
  def ${scope.getter}[T <: ${scope.rhstype}](db:SQLProc, rhs:Class[T] ): Option[T] = {
    val rc= db.getO2O(this, rhs, ${scope.colname} )
    setRef( ${scope.colname}, rc.getOrElse(null) )
    rc
  }

  def ${scope.getter}[T <: ${scope.rhstype}]( rhs:Class[T] ): Option[T] = {
    getRef(${scope.colname}) match {
      case x:T => Option(x)
      case _ => None
    }
  }

  def ${scope.setter}[T <: ${scope.rhstype}](p:T ) = setO2O( p, ${scope.colname} )

</#if>
<#if scope.o2m = true >
  <#if scope.singly = true>
  def ${scope.getter}(db:SQLProc): Option[${scope.rhstype}] = {
    val x = db.getO2M(this, classOf[${scope.rhstype}], ${scope.colname}) match {
      case s if s.size > 0 => Option(s(0))
      case _ => None
    }
    setRef( ${scope.colname} , x.getOrElse(null) )
    x
  }
  def ${scope.getter}(): Option[${scope.rhstype}] = {
    getRef(${scope.colname} ) match {
      case Some( x:${scope.rhstype} )  => Option(x)
      case _ => None
    }
  }
  def ${scope.setter}(p:${scope.rhstype} ) {
    linkO2M(p, ${scope.colname})
    setRef( ${scope.colname}, p)
  }
  def ${scope.delone}(d:${scope.rhstype} ) = {
    setRef(${scope.colname}, null)
    unlinkO2M(d, ${scope.colname})
  }
  def ${scope.delall}(db:SQLProc) = {
    setRef(${scope.colname}, null)
    db.purgeO2M(this, classOf[${scope.rhstype}], ${scope.colname} )
  }

  <#else>
  def ${scope.getter}(db:SQLProc): Seq[${scope.rhstype}] = {
    val rc= db.getO2M(this, classOf[${scope.rhstype}], ${scope.colname} )
    setRef("${scope.getter}", rc.toList )
    rc
  }
  def ${scope.getter}(): Seq[${scope.rhstype}] = {
    getSeq("${scope.getter}") match {
      case Some(x:Seq[_]) if x.size > 0 => x.asInstanceOf[Seq[${scope.rhstype}]]
      case _ => List()
    }
  }
  def ${scope.adder}(d:${scope.rhstype} ) = linkO2M(d, ${scope.colname} )
  def ${scope.delone}(d:${scope.rhstype} ) = unlinkO2M(d, ${scope.colname})
  def ${scope.delall}(db:SQLProc) = db.purgeO2M(this, classOf[${scope.rhstype}], ${scope.colname} )
  </#if>
</#if>

<#if scope.m2m = true >
  def ${scope.getter}( db:SQLProc) = db.getM2M(this, classOf[${scope.rhstype}] )

  def ${scope.getter}() = {
    getSeq("${scope.getter}") match {
      case Some(x:Seq[_]) if x.size > 0 => x.asInstanceOf[Seq[${scope.rhstype}]]
      case _ => List()
    }
  }

  def ${scope.delone}(db:SQLProc, e:${scope.rhstype} ) = db.unlinkM2M(this, e)

  def ${scope.delall}(db:SQLProc) = db.purgeM2M(this, classOf[${scope.rhstype}] )

  def ${scope.adder}(db:SQLProc, e:${scope.rhstype}) = db.linkM2M(this, e)

</#if>
