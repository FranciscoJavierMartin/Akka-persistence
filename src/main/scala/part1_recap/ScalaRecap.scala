package part1_recap

import scala.concurrent.Future
import scala.util.{Failure, Success}

object ScalaRecap extends App{

  val aCondition: Boolean = false

  def myFunction(x:Int) ={
    if(x>4) 42 else 65
  }

  class Animal
  trait Carnivore{
    def eat(a:Animal):Unit
  }

  object Carnivore

  // Generics
  abstract class MyList[+A]

  // Methods notations
  1 + 2 // Infix notation
  1.+(2)

  // Functional programming
  val anIncrementer: Int => Int = (x:Int) => x+1
  anIncrementer(1)

  List(1,2,3).map(anIncrementer)

  // Pattern matching
  val unknown: Any = 2
  val order = unknown match {
    case 1 => "first"
    case 2 => "second"
    case _ => "unknown"
  }

  try {
    // code that can throw an exception
    throw new RuntimeException
  }catch {
    case e:Exception => println("I caught one")
  }

  /**
    * Scala advanced
    */

  // Multithreading

  import scala.concurrent.ExecutionContext.Implicits.global
  val future = Future{
    // Long computation here
    // executed on SOME other thread
    42
  }

  // map, flatMap, filter + other niceties e.g. recover/recoverWith

  future.onComplete {
    case Success(value) => println(s"I found the meaing of the life: $value")
    case Failure(exception) => println(s"I found $exception while searching for the meaning of life")
  } // on SOME thread

  val partialFunction: PartialFunction[Int,Int] ={
    case 1 => 42
    case 2 => 65
    case _ => 999
  }

  // type aliases
  type AkkaReceive = PartialFunction[Any,Unit]
  def receive: AkkaReceive = {
    case 1 => println("hello!")
    case _ => println("confused...")
  }

  // Implicits
  implicit val timeout = 3000
  def setTimeout(f:()=>Unit)(implicit timeout:Int)=f()

  setTimeout(()=>println("Timeout")) // other arg list injected by the compiler

  // conversions
  // 1) implicit methods
  case class Person(name:String){
    def greet: String = s"Hi, my name is $name"
  }

  implicit def fromStringToPerson(name:String) = Person(name)
  "Peter".greet
  // fromStringToPerson("Peter").greet

  // 2) implicit classes
  implicit class Dog(name:String) {
    def bark = println("Bark!")
  }

  "Lassie".bark
  // new Dog("Lassie").bark

  // implicit organizations
  // local scope
  implicit val numberOrdering: Ordering[Int] = Ordering.fromLessThan(_>_)
  List(1,2,3).sorted // (numberOrdering) => List(3,2,1)

  // companion objects of the types involved in the call
  object Person {
    implicit val personOrdering: Ordering[Person] = Ordering.fromLessThan((a,b)=>a.name.compareTo(b.name)<0)
  }

  List(Person("Bob"),Person("Alice")).sorted // (Person.personOrdering)
  // => List(Person("Alice"),Person("Bob))
}