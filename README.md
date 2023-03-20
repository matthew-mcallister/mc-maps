# mc-maps

Software to generate, modify, and export minecraft maps offline.

## Building

To compile the program, you will have to obtain the deobfuscated
Minecraft `1.19.4.jar` and the Mojang libraries distributed alongside
it.

First, create a `libs` directory in this repository (`mkdir ./libs`).
Either copy or symlink the following files from Minecraft into this
dir:

```
authlib-3.18.38.jar
brigadier-1.0.18.jar
datafixerupper-6.0.6.jar
logging-1.1.1.jar
```

Now, download the deobfuscation mappings from the link provided in the
`downloads` section of the `1.19.4.json` file in the `versions` dir.
Use any of the various deobfuscation tools to create the deobfuscated
JAR and name it `1.19.4-deob.jar`. Finally, copy or link
`1.19.4-deob.jar` into `libs`.

Then just run `./gradlew build` to compile a runnable JAR.

TODO: Make everything version-agnostic.

# Copyright

Copyright (C) 2023 Matthew McAllister

mc-maps is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free
Software Foundation, either version 3 of the License, or (at your
option) any later version.

mc-maps is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
more details.
