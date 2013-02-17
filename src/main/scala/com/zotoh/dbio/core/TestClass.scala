package com.zotoh.dbio.core

import com.zotoh.dbio.meta.Table
import com.zotoh.dbio.meta.Column

object TestClass {
  def main(a:Array[String]) {
    new TestClass().setName("dsfsdf")
  }
}

@Table(table="crap")
class TestClass extends AbstractModel {
  
  @Column(desc="")
  def getName() = ""
    
  def setName(s:String) {    
  }
  
}