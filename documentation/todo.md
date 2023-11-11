# To Do
## Hp82240
### v1
* Extend unit tests.
* Printer impl that sends raw data to a file? Or creates a file that can be used by RedEyeSender?
* Character '̅x', needing 2 combined UTF-8 codes (0305 plus x). Currently rendered as an 'ẋ'.
* Buffer overflow: The real printer stops receiving after 200 bytes without a line feed, and prints a special character.
The simulator has way more memory, and does not yet simulate this behaviour.
* Make the name of the output files configurable (via command ine option).

### v2
* Convert to a web service (REST / JSON).
* Add a GUI (Angular or React).

## RedEyeSender
* Cleanup
