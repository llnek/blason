package com.zotoh.blason
package io

import com.zotoh.frwk.io.XData

class WebSockEvent(src:EventEmitter) extends AbstractEvent(src) {
  private var _data:XData=null
  private var _bin=false

  def setData( bin:Boolean, x:XData): this.type = {
    _data=x
    _bin=bin
    this
  }

  def setData(s:String): this.type= {
    _data= new XData(s)
    _bin=false
    this
  }

  def setData(b:Array[Byte]): this.type = {
    _data= new XData(b)
    _bin=true
    this
  }

  def data() = _data

}

