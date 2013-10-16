var eventQueue = new Array();
function setTimeout(f, delay) {
  eventQueue.push(f);
}

ScalaJS.modules.ch\ufe33epfl\ufe33lamp\ufe33scalajs\ufe33actors\ufe33test\ufe33hello\ufe33HelloActors().main();

while (eventQueue.length) {
  var event = eventQueue.shift();
  event();
}
