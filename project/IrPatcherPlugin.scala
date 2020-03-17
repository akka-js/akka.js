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

import org.scalajs.ir._
import Trees._
import Types._

import java.io._
import java.nio.file.{Files, Path}

object IrPatcherPlugin {

  // form:
  // https://github.com/scala-js/scala-js/blob/508dcf48910102cec222cf5201ca2450e2192da9/project/JavalibIRCleaner.scala#L124-L151
  private def readIRFile(path: Path): ClassDef = {
    import java.nio.ByteBuffer
    import java.nio.channels.FileChannel

    val channel = FileChannel.open(path)
    try {
      val fileSize = channel.size()
      if (fileSize > Int.MaxValue.toLong)
        throw new IOException(s"IR file too large: $path")
      val buffer = ByteBuffer.allocate(fileSize.toInt)
      channel.read(buffer)
      buffer.flip()
      Serializers.deserialize(buffer)
    } finally {
      channel.close()
    }
  }

  private def writeIRFile(path: Path, tree: ClassDef): Unit = {
    Files.createDirectories(path.getParent())
    val outputStream =
      new BufferedOutputStream(new FileOutputStream(path.toFile()))
    try {
      Serializers.serialize(outputStream, tree)
    } finally {
      outputStream.close()
    }
  }

  def patchHackedFile(file: File, hackFile: File): Unit = {

    val classDef = readIRFile(file.toPath())

    val className = classDef.name.name
    val classType = ClassType(className)

    val hackClassDef = readIRFile(hackFile.toPath())

    val hackClassType = ClassType(hackClassDef.name.name)

    val newMethods =
      hackClassDef.memberDefs filter { memberDef =>
        memberDef match {
          case MethodDef(_, hackIdent, _, _, _, _) =>
            !classDef.memberDefs.exists { md =>
              md match {
                case MethodDef(_, ident, _, _, _, _) =>
                  ident equals hackIdent
                case _ => false
              }
            }
          case FieldDef(_, hackIdent, _, _) =>
            !classDef.memberDefs.exists { md =>
              md match {
                case FieldDef(_, ident, _, _) =>
                  ident equals hackIdent
                case _ => false
              }
            }
          case _ => false
        }
      } map { d =>
        //this is to avoid copying position and to make akka-js-actor-ir-patches unreachable
        implicit val pos = Position.NoPosition
        d match {
          case MethodDef(flags, name, originalName, args, resultType, body) =>
            new MethodDef(flags, name, originalName, args, resultType, body)(Trees.OptimizerHints.empty, None)
          case FieldDef(static, name, ftpe, mutable) =>
            new FieldDef(static, name, ftpe, mutable)
          case any => throw new Exception("Not defined")
        }
      }

    val hackDefs =
      (classDef.memberDefs map { memberDef =>
        implicit val pos = memberDef.pos

        memberDef match {
          case FieldDef(stat, ident, tpe, mutable) =>
            val fieldH =
              hackClassDef.memberDefs find { md =>
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

    val newClassDef = new ClassDef(
      name = classDef.name,
      originalName = classDef.originalName,
      kind = classDef.kind,
      jsClassCaptures = classDef.jsClassCaptures,
      superClass = classDef.superClass,
      interfaces = classDef.interfaces,
      jsSuperClass = classDef.jsSuperClass,
      jsNativeLoadSpec = classDef.jsNativeLoadSpec,
      memberDefs = (hackDefs ++ newMethods),
      topLevelExportDefs = classDef.topLevelExportDefs
    )(optimizerHints = classDef.optimizerHints)(pos = classDef.pos)

    writeIRFile(file.toPath(), newClassDef)
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
}
