/* Copyright 2015 UniCredit S.p.A.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitmjations under the License.
*/

package org.akkajs

import java.io.File
import org.scalajs.core.ir._
import Trees._
import Types._
import org.scalajs.core.tools.io._

object IrPatcherPlugin {

  def patchHackedFile(file: File, hackFile: File): Unit = {

    val vfile = FileVirtualScalaJSIRFile(file)
    val (classInfo, classDef) = vfile.infoAndTree

    val className = classDef.name.name
    val classType = ClassType(className)

    val vHackfile = FileVirtualScalaJSIRFile(hackFile)
    val (hackClassInfo, hackClassDef) = vHackfile.infoAndTree
    val hackClassType = ClassType(hackClassDef.name.name)

    val newMethods =
      hackClassDef.defs filter { memberDef =>
        memberDef match {
          case MethodDef(_, hackIdent, _, _, _) =>
            !classDef.defs.exists { md =>
              md match {
                case MethodDef(_, ident, _, _, _) =>
                  ident equals hackIdent
                case _ => false
              }
            }
          case FieldDef(_, hackIdent, _, _) =>
            !classDef.defs.exists { md =>
              md match {
                case FieldDef(_, ident, _, _) =>
                  ident equals hackIdent
                case _ => false
              }
            }
          case _ => false
        }
      }

    val hackDefs =
      (classDef.defs map { memberDef =>
        implicit val pos = memberDef.pos

        memberDef match {
          case FieldDef(stat, ident, tpe, mutable) =>
            val fieldH =
              hackClassDef.defs find { md =>
                md match {
                  case FieldDef(stat, hackIdent, _, _) =>
                    hackIdent equals ident
                  case _ => false
                }
              }

            fieldH match {
              case Some(field @ FieldDef(stat, _, _, mut)) =>
                FieldDef(stat, ident, tpe, mut)
              case _ =>
                FieldDef(stat, ident, tpe, mutable)
            }
          case _ =>
            memberDef
        }
      })

    val newClassDef = classDef.copy(defs = (hackDefs ++ newMethods))(
      classDef.optimizerHints)(classDef.pos)

    val newClassInfo = Infos.generateClassInfo(newClassDef)

    val out = WritableFileVirtualBinaryFile(file)
    val outputStream = out.outputStream
    try {
      InfoSerializers.serialize(outputStream, newClassInfo)
      Serializers.serialize(outputStream, newClassDef)
    } finally {
      outputStream.close()
    }
  }

  def hackAllUnder(base: File, hack: File): Unit = {
    import scala.collection.JavaConversions._

    if (hack.isDirectory) {
      hack.listFiles.foreach(f =>
        hackAllUnder(new File(base.getAbsolutePath, f.getName), f)
      )
    } else if (hack.getAbsolutePath.endsWith(".sjsir")) {
      if (hack.exists && base.exists) {
        patchHackedFile(base, hack)
      }
    } else {}

  }

  def patchThis(classDir: File, configFile: File): Unit = {
    import java.nio.file.Files.readAllBytes
    import java.nio.file.Paths.get

    val hackClassDir = new File(new String(readAllBytes(get(configFile.getAbsolutePath))))

    hackAllUnder(classDir, hackClassDir)
  }
}
