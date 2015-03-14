package com.js.gest;

class StrokeSetEntry {

  public StrokeSet strokeSet() {
    return mStrokeSet;
  }

  public String name() {
    return mName;
  }

  /**
   * Get the name of the stroke set this one is an alias of; returns our name if
   * we are not an alias
   */
  String aliasName() {
    if (mAliasName == null)
      return mName;
    return mAliasName;
  }

  boolean hasAlias() {
    return mAliasName != null;
  }

  StrokeSetEntry(String name) {
    mName = name;
  }

  void setStrokeSet(StrokeSet strokeSet) {
    mStrokeSet = strokeSet;
  }

  void setAliasName(String name) {
    mAliasName = name;
  }

  private String mAliasName;
  private String mName;
  private StrokeSet mStrokeSet;
}
