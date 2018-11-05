# Notify Relay

Using Cipher Block Chaining (CBC) and strange phrases to publish 7 android
messages (all kind of messages) with the `notify_put` App. Process these
strange but secure coded messages with the c++ coded daemon `notify_store`.
Use on all Laptops or other Linux device the `notify_get` daemon, to
get and decode it and put them to the linux desktop message bus.

## Concept

- `notify_put` [APK](https://raw.githubusercontent.com/no-go/NotifyRelay/master/notify_put/release/notify_put-release.apk) coded TCP to `notify_store` save as file in a existing webserver
- `notify_get` TCP http GET from the webserver, decode, put to linux message bus
- servers only see coded stuff

![Sketch](concept.jpg)

## Bugs, Todos

- missing limit to store with `notify_store` and putting verification
- password is compiled in notify_get code

## License

This is free and unencumbered software released into the public domain.

Anyone is free to copy, modify, publish, use, compile, sell, or distribute this 
software, either in source code form or as a compiled binary, for any purpose, 
commercial or non-commercial, and by any means.

In jurisdictions that recognize copyright laws, the author or authors of this software 
dedicate any and all copyright interest in the software to the public domain. We make 
this dedication for the benefit of the public at large and to the detriment of our 
heirs and successors. We intend this dedication to be an overt act of relinquishment 
in perpetuity of all present and future rights to this software under copyright law.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, 
DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
DEALINGS IN THE SOFTWARE.

For more information, please refer to [http://unlicense.org](http://unlicense.org)
