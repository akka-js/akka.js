package com.typesafe.config

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.concurrent.duration.{ FiniteDuration, Duration, SECONDS }

object ConfigFactory {
  def parseString(s: String): Config = {
    new Config(JSON.parse(s))
  }
}

class Config(obj: js.Dynamic) {
  def this() = {
    this(JSON.parse("{}"))
  }
  
  private def getNested[A](path: String): A = {
    var tmp = obj.asInstanceOf[js.Object]
    /*path.split("\\.") foreach { p =>
      if(tmp.hasOwnProperty(p)) tmp = tmp.asInstanceOf[js.Dictionary[js.Any]](p).asInstanceOf[js.Object]
    }*/
    
    val res = path.split("\\.").foldLeft(obj.asInstanceOf[js.Object]){ (prev, part) =>
      if(prev.hasOwnProperty(part)) prev.asInstanceOf[js.Dictionary[js.Any]](part).asInstanceOf[js.Object]
      else prev
    }
    
    res.asInstanceOf[A]
  }
  
  def getString(path: String) = getNested[String](path)
  
  def getBoolean(path: String) = getNested[Boolean](path)
  
  def getMillisDuration(path: String) = {
    val res = getString(path)
    if(res.takeRight(1) == "s") {
      Duration(res.toInt, SECONDS)
    }
  }
  
  def getStringList(path: String) = getNested[js.Array[String]](path)

}