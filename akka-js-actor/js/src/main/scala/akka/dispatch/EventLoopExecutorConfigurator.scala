package akka.dispatch

import com.typesafe.config.Config
import java.util.concurrent.ExecutorService

class EventLoopExecutorConfigurator(config: Config, prerequisites: DispatcherPrerequisites) extends ExecutorServiceConfigurator(config, prerequisites) {

  def createExecutorServiceFactory(id: String): ExecutorServiceFactory =
    new ExecutorServiceFactory {
      def createExecutorService: ExecutorService = {
        new EventLoopExecutor
      }
    }
}
