package com.typesafe.config

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.concurrent.duration._
import scala.concurrent.{ Promise, Future }
import scala.util.{ Success, Failure }

object ConfigFactory {
  def parseString(s: String): Config = {
    new Config(JSON.parse(s))
  }
}

class Config(obj: js.Dynamic) {
  val fallback = Promise[Config]
  
  def this() = {
    this(JSON.parse("{}"))
  }
  
  def withFallback(c: Config) = {
    fallback.success(c)
    this
  } 
  
  private def getNested[A](path: String): A = {
    /*var tmp = obj.asInstanceOf[js.Object]
    path.split("\\.") foreach { p =>
      if(tmp.hasOwnProperty(p)) tmp = tmp.asInstanceOf[js.Dictionary[js.Any]](p).asInstanceOf[js.Object]
    }*/
    
    try {
      val res = path.split("\\.").foldLeft(obj.asInstanceOf[js.Object]){ (prev, part) =>
          /*if(prev.hasOwnProperty(part))*/ prev.asInstanceOf[js.Dictionary[js.Any]](part).asInstanceOf[js.Object]
          //else prev
      } 
    
      res.asInstanceOf[A]
    } catch {
      case e: NoSuchElementException => 
        if(!fallback.isCompleted) null.asInstanceOf[A]
        else {
          fallback.future.value match {
            case Some(Success(c)) => c.getNested[A](path)
          }
        }
    }
  }
  
  def getString(path: String) = getNested[String](path)
  
  def getBoolean(path: String) = getNested[Boolean](path)
  
  def getInt(path: String) = getNested[Int](path)
  
  def getNanosDuration(path: String) = {
    val res = getString(path)
    Duration(res.toInt, NANOSECONDS)
  }
  
  def getMillisDuration(path: String) = {
    val res = getString(path)
    Duration(res.toInt, MILLISECONDS)
  }
  
  def getStringList(path: String) = getNested[js.Array[String]](path)

}