public class grScript extends groovy.lang.Script {
public static void main(java.lang.String[] args) {
new grScript(new groovy.lang.Binding(args)).run();
}

public java.lang.Object run() {
print("foo");
if (true){
print("false");
return null;
}
 else {
print("true");
java.lang.Integer a = 5;
}


}

public grScript(groovy.lang.Binding binding) {
super(binding);
}
public grScript() {
super();
}
}
