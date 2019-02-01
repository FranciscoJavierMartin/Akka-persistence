package part2_event_sourcing

import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.PersistentActor

object MultiplePersists extends App {

  /*
    Diligent accountant: with every invoice, will persist two events
    -  a tax record for the fiscal authority
    - an invoice record for personal logs or some auditing authority
   */

  // Command
  case class Invoice(recipient: String, date: Date, amount: Int)

  // Events
  case class TaxRecord(taxId: String, recordId: Int, date: Date, totalAmount: Int)
  case class InvoiceRecord(invoiceRecordId: Int, recipient: String, date: Date, amount: Int)

  object DiligentAccountant{
    def props(taxId:String, taxAuthority: ActorRef)= Props(new DiligentAccountant(taxId,taxAuthority))
  }

  class DiligentAccountant(taxId:String,taxAuthority: ActorRef) extends PersistentActor with ActorLogging {

    var latestTaxRecord = 0
    var latestInvoiceRecordId = 0

    override def persistenceId: String = "diligent-accountant"

    override def receiveCommand: Receive = {
      case Invoice(recipient,date,amount) =>
        // journal ! TaxRecord
      persist(TaxRecord(taxId,latestTaxRecord,date,amount/3)) { record =>
          taxAuthority ! record
          latestTaxRecord += 1

          persist("I hereby declare this tax record to be true and complete.") { declaration =>
            taxAuthority ! declaration
          }
        }

        // journal ! Invoice
        persist(InvoiceRecord(latestInvoiceRecordId, recipient, date, amount)) { invoiceRecord =>
          taxAuthority ! invoiceRecord
          latestInvoiceRecordId += 1

          persist("I hereby declare this tax record to be true.") { declaration =>
            taxAuthority ! declaration
          }
        }
    }

    override def receiveRecover: Receive = {
      case event => log.info(s"Recovered: $event")
    }

  }

  class TaxAuthority extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(s"Received: $message")
    }
  }

  val system = ActorSystem("MultiplePersistsDemo")
  val taxAuthority = system.actorOf(Props[TaxAuthority],"HMRC")
  val accountant = system.actorOf(DiligentAccountant.props("UK52352_58325",taxAuthority))

  accountant ! Invoice("The Sofa company",new Date, 2000)
  accountant ! Invoice("ACME", new Date, 12345)
  /*
    The message ordering (TaxRecord -> InvoiceRecord) is guaranteed.
   */

  // Nested persistent
}
