package part2_event_sourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, Recovery, RecoveryCompleted, SnapshotSelectionCriteria}

object RecoveryDemo extends App{

  case class Command(contents: String)
  case class Event(id: Int, contents: String)

  class RecoveryActor extends PersistentActor with ActorLogging{

    override def persistenceId: String = "recovery-actor"

    override def receiveCommand: Receive = online(0)

    def online(latestPersistedEventId:Int): Receive = {
      case Command(contents) =>
        persist(Event(latestPersistedEventId, contents)) { event =>
          log.info(s"Successfully persisted $event, recovery is ${if (this.recoveryFinished)"" else "not" }finished.")
          context.become(online(latestPersistedEventId+1))
        }
    }

    override def receiveRecover: Receive = {
      case RecoveryCompleted =>
        // additional initialization
        log.info("I have finished recovering")
      case Event(id,contents) =>
        // It's for test failure recovery
//        if(contents.contains("314"))
//          throw new RuntimeException("I can't take this anymore")
        log.info(s"Recovered: $contents, recovery is ${if (this.recoveryFinished)"" else "not" }finished.")
        context.become(online(id + 1))
        /*
          This will not change the event handler during recovery
          After recovery the "normal" handler will be the result of all the stacking of context.become
         */
    }



    override def onRecoveryFailure(cause: Throwable, event: Option[Any]): Unit = {
      log.error("I failed at recovery")
      super.onRecoveryFailure(cause, event)
    }

//    override def recovery: Recovery = Recovery(toSequenceNr = 100)
//    override def recovery: Recovery = Recovery(fromSnapshot = SnapshotSelectionCriteria.Latest)
//    override def recovery: Recovery = Recovery.none
  }

  val system = ActorSystem("RecoveryDemo")
  val recoveryActor = system.actorOf(Props[RecoveryActor],"recoveryActor");

  /*
    Stashing commands
   */
//  for(i <- 1 to 1000) {
//    recoveryActor ! Command(s"command $i")
//  }

  // All commands sent during recovery are stashed

  /*
    2 - Failure during recovery
      - onRecoveryFailure + the actor is stopped
   */

  /*
    3 - customizing recovery
      - Do not persist more events after a customized _incomplete_ recovery
   */

  /*
    4 - recovery status or knowing when you are done recovering
      - getting a signal when you are done recovering
   */
  /*
    5 - stateless actors
   */

  recoveryActor ! Command(s"Special command 1")
  recoveryActor ! Command(s"Special command 2")
}
