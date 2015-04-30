package com.typesafe.config

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.concurrent.duration.{ FiniteDuration, Duration, SECONDS }

class ConfigFactory {
  def parseString(s: String) = {
    val parsedJSON = JSON.parse(s)
    
  }
}

class Config(obj: js.Dynamic) {
  private def getNested[A](path: String): A = {
    var tmp = obj.asInstanceOf[js.Object]
    path.split("\\.") foreach { p =>
      if(tmp.hasOwnProperty(p)) tmp = tmp.asInstanceOf[js.Dictionary[js.Any]](p).asInstanceOf[js.Object]
    }
    
    tmp.asInstanceOf[A]
  }
  
  def getString(path: String) = getNested[String](path)
  
  def getBoolean(path: String) = getNested[Boolean](path)
  
  def getMillisDuration(path: String) = {
    val res = getString(path)
    if(res.takeRight(1) == "s") {
      Duration(res.toInt, SECONDS)
    }
  }
  
  def getStringList(path: String) = ???

}