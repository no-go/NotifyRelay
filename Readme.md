# Notify Relay

Using Cipher Block Chaining (CBC) and strange phrases to publish 7 android
messages (all kind of messages) with the `notify_put` App. Process these
strange but secure coded messages with the c++ coded daemon `notify_store`.
Use on all Laptops or other Linux device the `notify_get` daemon, to
get and decode it and put them to the linux desktop message bus.

## Concept

- `notify_put` coded TCP to `notify_store` save as file in a existing webserver
- `notify_get` TCP http GET from the webserver, decode, put to linux message bus
- servers only see coded stuff


## Bugs, Todos

- utf8 not working
- missing a timestamp
- password is compiled in notify_get code
