// workaround for https://github.com/scala-js/scala-js/issues/172
ScalaJS.c.java_lang_Long$.prototype.numberOfLeadingZeros__J__I = function(l) {
  // hack into a private method, not pretty ...
  return l.scala$scalajs$runtime$Long$$numberOfLeadingZeros__I();
};

// startup
ScalaJS.modules.client_Main().startup();
