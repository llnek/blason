<#if scope.col = true >
  def ${scope.setter}(p:${scope.param_type}):Unit = writeData(${scope.colname}, Option(p))
  @Column(${scope.coldetails})
  def ${scope.getter}() = ${scope.reader}(${scope.colname})

</#if>
<#if scope.o2o = true >
  @One2One(rhs=classOf[${scope.rhstype}])
  def ${scope.getter}[T <: ${scope.rhstype}](db:SQLProc, rhs:Class[T], cache:Boolean = false )(implicit m: Manifest[T]): Option[T] = {
    if (cache) getRef(${scope.colname}) match {
      case x:T => return Option(x)
      case _ =>
    }
    val rc= db.getO2O(this, rhs, ${scope.colname} )
    setRef( ${scope.colname}, rc.getOrElse(null) )
    rc
  }
  def ${scope.setter}[T <: ${scope.rhstype}](p:T ):Unit = setO2O( p, ${scope.colname} )

</#if>
<#if scope.o2m = true >
  @One2Many(rhs=classOf[${scope.rhstype}])
  def ${scope.getter}(db:SQLProc, cache:Boolean = false): Seq[${scope.rhstype}] = {
    if (cache) getSeq("${scope.getter}") match {
      case x:Seq[_] if x.size > 0 => return x.asInstanceOf[Seq[${scope.rhstype}]]
      case _ =>
    }
    val rc= db.getO2M(this, classOf[${scope.rhstype}], ${scope.colname} )
    setRef("${scope.getter}", rc.toList )
    rc
  }
  def ${scope.adder}(d:${scope.rhstype} ):Unit = linkO2M(d, ${scope.colname} )
  def ${scope.delone}(d:${scope.rhstype} ):Unit = unlinkO2M(d, ${scope.colname})
  def ${scope.delall}(db:SQLProc):Unit = db.purgeO2M(this, classOf[${scope.rhstype}], ${scope.colname} )

</#if>
<#if scope.m2m = true >
  @Many2Many(rhs=classOf[${scope.rhstype}] ,joined=classOf[${scope.joinedtype}])
  def ${scope.getter}( db:SQLProc) = db.getM2M(this, classOf[${scope.rhstype}] )
  def ${scope.delone}(db:SQLProc, e:${scope.rhstype} ):Unit = db.unlinkM2M(this, e)
  def ${scope.delall}(db:SQLProc):Unit = db.purgeM2M(this, classOf[${scope.rhstype}] )
  def ${scope.adder}(db:SQLProc, e:${scope.rhstype}):Unit = db.linkM2M(this, e)

</#if>
