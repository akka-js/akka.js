var console = console || {
  log: function() {},
  error: function() {}
};
console.log("Worker loading ...")
importScripts(
  "./target/scala-2.10/scalajs-actors-example-webworkers-fastopt.js");
WebWorkerMain().main();
