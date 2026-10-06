/**
 * The slice of three.js the payment journey draws with. The engine and the
 * model builders take the library as a namespace argument (`T.Mesh`), which
 * would pull in all of three; naming the members here lets the bundler drop
 * the rest of the library from the lazy chunk.
 */
export {
  ACESFilmicToneMapping, AdditiveBlending, BoxGeometry, BufferAttribute, BufferGeometry,
  CanvasTexture, Color, CubicBezierCurve3, CylinderGeometry, DirectionalLight, DoubleSide,
  EdgesGeometry, Euler, ExtrudeGeometry, Float32BufferAttribute, Group, HemisphereLight,
  IcosahedronGeometry, InstancedMesh, Line, LineBasicMaterial, LineLoop, LineSegments,
  Matrix4, Mesh, MeshBasicMaterial, MeshPhysicalMaterial, MeshStandardMaterial, Object3D,
  OrthographicCamera, PCFSoftShadowMap, PMREMGenerator, Path, PlaneGeometry, PointLight,
  Points, PointsMaterial, Quaternion, SRGBColorSpace, Scene, ShadowMaterial, Shape,
  ShapeGeometry, SphereGeometry, Sprite, SpriteMaterial, TorusGeometry, Vector3,
  WebGLRenderer, WireframeGeometry,
} from 'three';
