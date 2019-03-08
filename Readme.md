# Notify to Jabber

![Logo](app/src/main/res/mipmap-xhdpi/ic_launcher.png)

Sometimes a new giant smartphone is too big for me at parties. It's nice to 
be able to redirect all your messages to a little old one. In an emergency 
you can at least answer via SMS. My app redirects almost everything to a 
Jabber account, to your Gotify server or as sms. It's a little everyday hack that I
don't want to keep from the FOSS community.

The App sends all Android text notifications to a xmpp jabber account. It
uses [smack](https://github.com/igniterealtime/Smack/) lib for this.

Alternative: use your [gotify server](https://github.com/gotify/server) and an application token to
submit the notifications. It uses the [Retrofit](https://github.com/square/retrofit) lib for the REST request.

## Read from Gotify

I improved a Android [Gotify Read](https://gitlab.com/deadlockz/gotifyread) App, to read and delete messages. Additionaly I improve a 
[Desktop App](https://gitlab.com/deadlockz/gotifyread/tree/master/Desktop) with nodeJS
and [electron](https://electronjs.org/) to read e.g. the playing music titles on my notebook. 

[GotifyRead Sources](https://gitlab.com/deadlockz/gotifyread)

## Gotify with self signed cert

If a X509Certificate is detected, domain is checked and fingerprint is store.
Additional access will compare these fingerprint.

## Get the App

You can get a signed APK (4.4.4+) from [here](https://raw.githubusercontent.com/no-go/NotifyRelay/master/app/release/click.dummer.notify_to_jabber.apk)

Alternative:

<a href="https://f-droid.org/packages/click.dummer.notify_to_jabber/" target="_blank">
<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80"/></a>

## My App License

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

For more information: [http://unlicense.org](http://unlicense.org)

## Smack Library License

    Use of the Smack source code is governed by the Apache License Version 2.0:

    Copyright 2002-2008 Jive Software.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

## Retrofit Library License

    Copyright 2013 Square, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.