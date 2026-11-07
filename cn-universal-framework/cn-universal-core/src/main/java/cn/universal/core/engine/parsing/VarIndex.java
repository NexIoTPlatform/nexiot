package cn.universal.core.engine.parsing;

import java.util.Objects;

public class VarIndex {

  private final String name;

  private final int index;

  private final boolean reference;

  private final boolean readonly;

  private final boolean scoped;

  VarIndex(String name, int index, boolean reference) {
    this(name, index, reference, false);
  }

  public VarIndex(String name, int index, boolean reference, boolean readonly) {
    this(name, index, reference, readonly, false);
  }

  public VarIndex(String name, int index, boolean reference, boolean readonly, boolean scoped) {
    this.name = name;
    this.index = index;
    this.reference = reference;
    this.readonly = readonly;
    this.scoped = scoped;
  }

  public String getName() {
    return name;
  }

  public int getIndex() {
    return index;
  }

  public boolean isReference() {
    return reference;
  }

  public boolean isReadonly() {
    return readonly;
  }

  public boolean isScoped() {
    return scoped;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof VarIndex)) {
      return false;
    }
    VarIndex varIndex = (VarIndex) o;
    return index == varIndex.index
        && reference == varIndex.reference
        && Objects.equals(name, varIndex.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, index, reference);
  }

  public VarIndex scoped() {
    return new VarIndex(this.name, this.index, this.reference, this.readonly, true);
  }
}
