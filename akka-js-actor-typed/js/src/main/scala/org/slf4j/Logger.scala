package org.slf4j

class Logger {

  def trace(args: Any*) = {
    args.foreach( x => print(x.toString))
    print("\n")
  }
  def debug(args: Any*) = {
    args.foreach( x => print(x.toString))
    print("\n")
  }
  def info(args: Any*) = {
    args.foreach( x => print(x.toString))
    print("\n")
  }
  def warn(args: Any*) = {
    args.foreach( x => print(x.toString))
    print("\n")
  }
  def error(args: Any*) = {
    args.foreach( x => print(x.toString))
    print("\n")
  }
}