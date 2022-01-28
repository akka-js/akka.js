package akka.dispatch

import com.typesafe.config.Config
import org.scalajs.macrotaskexecutor.MacrotaskExecutor

import java.util.concurrent.ExecutorService
import scala.scalajs.reflect.annotation.EnableReflectiveInstantiation

@EnableReflectiveInstantiation
class MacrotaskExecutorConfigurator(config: Config, prerequisites: DispatcherPrerequisites) extends ExecutorServiceConfigurator(config, prerequisites) {

  private lazy val factory = new ExecutorServiceFactory {
    def createExecutorService: ExecutorService = new ExecutionContextExecutorServiceDelegate(MacrotaskExecutor)
  }

  def createExecutorServiceFactory(id: String): ExecutorServiceFactory = factory
}
