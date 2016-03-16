# hobby-code
Stand alone snippets that I use.

## ImageViewer.java
A small javafx application that loads all of the image files from a directory. When each
file is set to be displayed, the image is loaded to a SoftReference. Subsequent loading
might be skipped if the soft reference never gets cleared. 