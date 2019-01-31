package part2_event_sourcing

import java.util.Date

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor

object PersistentActors extends App{

  /*
    Scenario: we have a business and an accountant which keeps track of our invoices.
   */

  // Commands
  case class Invoice(recipient: String, date: Date, amount: Int)
  case class InvoiceBulk(invoices: List[Invoice])

  // Special messages
  case object Shutdown

  // Events
  case class InvoiceRecorded(id:Int,recipient:String,date:Date,amount:Int)

  class Accountant extends PersistentActor with ActorLogging {

    var latestInvoiceId = 0
    var totalAmount = 0

    override def persistenceId: String = "simple-accountant" // best practice: make it unique

    /*
      The "normal" receive method
     */
    override def receiveCommand: Receive = {
      case Invoice(recipient, date, amount) =>
        /*
          When you receive a command
          1) You create an event to persist into the store
          2) You persist the event, the pass in a callback that will get triggered once the event is written
          3) We update the actor's state when the event has persisted
         */
        log.info(s"Receive invoice for amount: $amount")
        val event = InvoiceRecorded(latestInvoiceId,recipient,date,amount)
        persist(event) { e =>
          // time gap: all other messages sent to this actor are stashed
          // SAFE to access mutable state here
          // Update state
          latestInvoiceId += 1
          totalAmount += amount
          // correctly identify the sender of the command
          sender() ! "PersistenceACK"
          log.info(s"Persisted $e as invoice #${e.id}, for total amount $totalAmount")
        }
      case InvoiceBulk(invoices) =>
        /*
          1) Create events (plural)
          2) Persist all the events
          3) Update the actor state when each event is persisted
         */
        val invoiceIds = latestInvoiceId to (latestInvoiceId + invoices.size)
        val events = invoices.zip(invoiceIds).map { pair =>
          val id = pair._2
          val invoice = pair._1
          InvoiceRecorded(id,invoice.recipient,invoice.date,invoice.amount)
        }
        persistAll(events){ e =>
          latestInvoiceId+=1
          totalAmount+=e.amount
          log.info(s"Persisted single $e as invoice #${e.id}, for total amount $totalAmount")
        }
      case Shutdown =>
        context.stop(self)
      // Act like a normal actor
      case "print" =>
        log.info(s"Latest invoice id: $latestInvoiceId, total amount: $totalAmount")
    }

    /*
      Handler that will be called on recovery
     */
    override def receiveRecover: Receive = {
      /*
        Best practice: follow the logic in the persist steps of receiveCommand
       */
      case InvoiceRecorded(id,_,_,amount) =>
        latestInvoiceId += 1
        totalAmount+=amount
        log.info(s"Recovered invoice #$id for amount $amount, total amount: $totalAmount")
    }

    /*
      This method is called if the persisting failed.
      The actor will be stopped.

      Best practice: start the actor again after a while
      (use Backoff supervisor)
     */
    override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"Fail to persist $event because of $cause")
      super.onPersistFailure(cause,event,seqNr)
    }

    /*
      Called if the journal fails to persist the event
     */
    override def onPersistRejected(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"Persist rejected for $event because of $cause")
      super.onPersistRejected(cause, event, seqNr)
    }
  }

  val system = ActorSystem("PersistentActors")
  val accountant = system.actorOf(Props[Accountant], "simpleAccountant")

//  for(i<-1 to 10) {
//    accountant ! Invoice("The Sofa Company", new Date, i * 1000)
//  }

  /*
    Persistence failure
   */

  /**
    * Persisting multiple events
    *
    * persistAll
    */

  val newInvoices = for (i <- 1 to 5) yield Invoice("The awesome chairs", new Date, i* 2000)
//  accountant ! InvoiceBulk(newInvoices.toList)

  /*
    Never call persist or persistall from futures
   */

  /**
    * Shutdown of persist actors
    *
    * Best practice: define your own "shutdown" messages
    */
  accountant ! Shutdown
}
