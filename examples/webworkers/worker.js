var console = console || {
  log: function() {},
  error: function() {}
};
console.log("Worker loading ...")
importScripts(
  "./target/scala-2.11.0-M7/scalajs-actors-example-webworkers-extdeps.js",
  "./target/scala-2.11.0-M7/scalajs-actors-example-webworkers-intdeps.js",
  "./target/scala-2.11.0-M7/scalajs-actors-example-webworkers.js")
ScalaJS.modules.org_scalajs_examples_webworkers_WebWorkerMain().main();
