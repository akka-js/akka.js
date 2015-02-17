var console = console || {
  log: function() {},
  error: function() {}
};
console.log("Worker loading ...")
importScripts(
  "./target/scala-2.11/scalajs-actors-example-webworkers-fastopt.js");
org.scalajs.examples.webworkers.WebWorkerMain().main();
