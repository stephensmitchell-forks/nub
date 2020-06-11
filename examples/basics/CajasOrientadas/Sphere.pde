class Sphere extends Node {
  float _radius;
  int _color;

  public Sphere(int tint, float radius) {
    _color = tint;
    _radius = radius;
    setPickingPolicy(Node.PickingPolicy.PRECISE);
    enableHint(Node.IMR | Node.RMR | Node.HIGHLIGHT);
  }

  @Override
  public void graphics(PGraphics pg) {
    pg.pushStyle();
    pg.noStroke();
    pg.fill(_color);
    pg.sphere(_radius);
    pg.popStyle();
  }
}
