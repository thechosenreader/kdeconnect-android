In deciding the syntax for arguments, I went for something that would be both concise and not conflict with existing bash syntax. In the end, I decided on the following:

`$$name[="DEFAULTVALUE"]`

Though `$$` is reserved in bash, it's unlikely to be useful in the context of the Run Command plugin and so felt appropriate. The name can be alphanumeric and contain underscores (note, however, that a leading underscore has a special meaning, see below). It may start with a digit. To supply a default value, simply suffix the name with an equals sign `=` and then wrap the value in double quotes. The quotes *must* be supplied. Within the quotes, anything goes. To use a double quote within the default value, escape it with a backslash. Some examples..

- **VALID**
  * `echo $$msg="hi"`
  * `updog -d $$_directory="/path/to/directory"`  
  * `echo $$dialog_with_underscores_121212="you can call me \"joe\""`
  * `echo $$1arg1`


- **INVALID**
  * `updog -p $$port=9001` // default value not in quotes
  * `echo $$msg="sdsdsd` // quotes not closed properly


When supplying the values to an argument, if any specific argument is not defined, an empty string is used in its place. When multiple default values are defined, the rightmost one is used for *all* occurrences.

By default, supplied values are always wrapped in quotes. As that functionally escapes the whitespace, it can cause problems when using named arguments in command substitution (e.g `$($$cmd)`). To prevent supplied values being wrapped in quotes, prefix the name of the argument with an underscore. Thus, `$($$cmd)` will fail when `du -chLs directory` is passed but `$($$_cmd)` will not.
