import java.beans.PropertyChangeSupport;
import lombok.Setter;
import lombok.experimental.Accessors;
class SetterBoundPlain {
  public static final java.lang.String PROP_FOO = "foo";
  private final PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);
  @Setter @Accessors(bound = true) int foo;
  <clinit>() {
  }
  SetterBoundPlain() {
    super();
  }
  public @java.lang.SuppressWarnings("all") void setFoo(final int foo) {
    this.foo = foo;
  }
}
