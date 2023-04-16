# img_dumpster

I take a lot of photos on my phone and I want a system that
can automatically extract, categorise and store my photos 
away for me. All I want at the end of the day is to put my
phone down and this system starts taking the photos out.

Kinda like Google photos but without the Google part.

I intend to write this in two ways:
1. A backend server that is wired up to some external hard
drives
2. Some way of read the phones filesystem and uploading the
photos out to the above web server

I can use exif to extract all the metadata from the photos
that are uploaded and I could potentially use a database to
track any photos that I may have doubled up on or search for.

My choice of the combined tech are:
1. A rust actix-web web server
2. An android jetpack compose app

I wanted to go for a web app, but a web app does not have
access to the phones file system so I will have to go with
a native app.

## Update: 16th of April, 2023

It turns out that this current build of this project semi works
for Android 25, but the contentprovider doesn't delete the files
for some odd reason.

As of this video: https://www.youtube.com/watch?v=Y2lX-UNxwbE
apparently Google have decided to change how storage works all
together and this is where I am throwing in the towel.

It was an absolute nightmare to get the android app this far.
And now the APIs are not even stable? and can change between
versions?

Then I am done with the app, screw the app.

I will just work with the rust webserver at least, I will need
an external SSD to store this info, I may end up going for an
external drive to store these photos somewhere and I will just use
the Android Transfer Tool to read these files off the phone.