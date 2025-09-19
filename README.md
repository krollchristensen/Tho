start to klienter i hver sin terminal
java ChatClient localhost 5555 alice
java ChatClient localhost 5555 bob

fra alice:
/sendfile bob C:\sti\til\en\fil.pdf

hos bob:
/accept <transferId> C:\hvor\den\skal\ligge\fil.pdf
eller:
/reject <transferId>

protokol: én JSON pr. linje, fil-data i base64-chunks, transferId og seq bevaret.

ingen packages og ingen eksterne biblioteker, så det er nemt at kompilere og køre hvor som helst.

når I vil, kan Json udskiftes med Jackson/Gson ved blot at ændre parse/stringify-kald i koden.

