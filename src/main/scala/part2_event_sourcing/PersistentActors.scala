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

  }

  val system = ActorSystem("PersistentActors")
  val accountant = system.actorOf(Props[Accountant], "simpleAccountant")

//  for(i<-1 to 10) {
//    accountant ! Invoice("The Sofa Company", new Date, i * 1000)
//  }
}
