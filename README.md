![AUR](https://img.shields.io/aur/license/yaourt.svg)
![AUR](https://img.shields.io/badge/platform-android-green.svg)
![AUR](https://img.shields.io/badge/Release-1.1-lightgrey.svg)

# k3pler proxy

### Android network connection blocker and packet analyzer built on top of local HTTP proxy.

<img src="https://github.com/KeyLo99/k3pler/blob/master/etc/k3plerbg3_splash.png" width="270" height="385"/>

### Features
* Show network traffic in a list (HTTP Request/Response)

<img src="https://github.com/KeyLo99/k3pler/blob/master/etc/page1.png" width="341" height="606"/>

* Gives details about requests/responses with abbreviations in list.
(eg: ```example.com ~ C [H/1.1] _S_ {time}```)
(C -> Connect | H/1.1 -> HTTP version | _S_ -> Success)
* Blacklist connection after getting detailed information
* Edit/Remove item at blacklist - clear blacklist

<img src="https://github.com/KeyLo99/k3pler/blob/master/etc/page2.png" width="341" height="606"/>

* Settings:
    ```
    Proxy Port: Local Proxy port for connection
    Max. Buffer: Maximum response buffer size in bytes
    Match Type:
        Full: Block if request URI equals blacklist item
        Keyword: Block if request URI contains blacklist item
    Blacklist Response: Response status which will be sent to blocked address
    Splash Screen: Show splash screen when app starts
    ```

<img src="https://github.com/KeyLo99/k3pler/blob/master/etc/page3.png" width="341" height="606"/>

* Shows instructions of configuring proxy (WIFI / Mobile Network)

### Licenses
* [Android v7 RecyclerView Library](https://developer.android.com/topic/libraries/support-library/packages) `(Apache 2.0)`
* [LittleProxy 1.1.2](https://github.com/adamfisk/LittleProxy) `(Apache 2.0)`
