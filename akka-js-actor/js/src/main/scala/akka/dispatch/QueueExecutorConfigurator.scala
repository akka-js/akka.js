package akka.dispatch

import com.typesafe.config.Config

import java.util.concurrent.ExecutorService
import scala.scalajs.concurrent.JSExecutionContext
import scala.scalajs.reflect.annotation.EnableReflectiveInstantiation

@EnableReflectiveInstantiation
class QueueExecutorConfigurator(config: Config, prerequisites: DispatcherPrerequisites) extends ExecutorServiceConfigurator(config, prerequisites) {

  private lazy val factory = new ExecutorServiceFactory {
    def createExecutorService: ExecutorService = new ExecutionContextExecutorServiceDelegate(JSExecutionContext.queue)
  }

  def createExecutorServiceFactory(id: String): ExecutorServiceFactory = factory
}
