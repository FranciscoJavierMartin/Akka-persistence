# Akka persistance

Code from the Akka persistence course

## Basics
- ```onPersistFailure```
    - called when sending the event fails
    - actor is stopped
- ```onPersistRejected```
    - called when the journal falls to write an event
    - actor is resumed
- ```persistAll```
    - send multiple events in sequence, atomically
    - handle each persisted event in the callback
    
- Never call persist from futures
- Shutdown of persistent actors

## Multiple persist
Persistence is based on messages. Think of persisting as sending a message to the journal.

- Calls to ```persist()``` are executed in order
- Handlers for subsequent ```persist()``` calls are executed in order

## Snapshot
Save the entire state as checkpoints (snapshots). Recover the last snapshot and events since then.

Saving snapshots:
- dedicated store
- asynchronous
- can fail, but no big deal

## Recovery
- Messages (commands) sent during recovery are stashed.
- If recovery fails, *onRecoveryFailure* is called and the actor is stopped.
- You can customize recovery.
```scala
   override def recovery: Recovery = Recovery(fromSequenceNr = 100)
```
- Or even disable recovery.
```scala
   override def recovery: Recovery = Recovery.none
```
- Get a signal when recovery is completed.
```scala
    case RecoveryCompleted => ...
```
- To use with stateless actors
    - use *context.become*  in *receiveCommand* (like normal actors).
    - also fine in *receiveRecover*, but the last handler will be used, and after a recovery.