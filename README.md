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