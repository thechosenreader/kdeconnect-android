As you'll see, this feature relies on functionality of bash. It will probably not work on Windows. Even though the named arguments should in theory, they are executed in an identical manner as to positional arguments (for simplicity reasons) and thus won't work.

# Positional Arguments
The syntax for positional arguments follows *exactly* the bash syntax. This is not a coincidence. All commands that contain arguments (even those with only named arguments) are executed by wrapping them in a function definition and then calling that function. For example, `echo $1 $2 $$msg` would be executed as `randfuncname() { echo $1 $2 "MSGVALUE" }; randfuncname "arg1" "arg2"`. In case you need a refresher on bash positional arguments, `$1` through `$9` reference the first 9 positional arguments. After that, the index of the argument must be wrapped in curly brackets, like so `${10}`. `$10` is NOT the tenth argument, it is the first concatenated with 0. Indices 1-9 can also be placed in the curly brackets but it is optional.


# Named Arguments
In deciding the syntax for named arguments, I went for something that would be both concise and not conflict with existing Bbashash syntax. In the end, I decided on the following:

`$$name[="DEFAULTVALUE"]`

Though `$$` is reserved in bash, it's unlikely to be useful in the context of the Run Command plugin and so felt appropriate. The name can be alphanumeric and contain underscores but may not start with a digit. To supply a default value, simply suffix the name with an equals sign `=` and then wrap the value in double quotes. The quotes *must* be supplied. Within the quotes, anything goes. To use a double quote within the default value, escape it with a backslash. Some examples..

- **VALID**
  * `echo $$msg="hi"`
  * `updog -d $$_directory="/path/to/directory"`
  * `echo $$dialog_with_underscores_121212="you can call me \"joe\""`

- **INVALID**
  * `echo $$1arg1`  // name starts with a digit
  * `updog -p $$port=9001` // default value not in quotes
  * `echo $$msg="sdsdsd` // quotes not closed properly


When supplying the values to an argument, if any specific argument is not defined, an empty string is used in its place.
