// @ts-nocheck
/* Ported verbatim from the design system (templates/_journey/scene-batch.js). */
/**
 * Merge static opaque meshes in place, retaining the original group transform.
 * Exclude any node whose transform/visibility changes after this call; its whole
 * subtree stays untouched. Materials (including live CanvasTextures) are reused.
 * Original geometries are never disposed because other actors may share them.
 */
export function batchStaticChildren(THREE, group, { exclude = [] } = {}) {
  const excluded = new Set(exclude.filter(Boolean));
  const buckets = new Map();
  const stats = { before: 0, after: 0, mergedMeshes: 0, batches: 0 };
  group.updateWorldMatrix(true, true);
  const inverse = group.matrixWorld.clone().invert();
  const inheritedState = object => {
    for (let parent = object; parent && parent !== group; parent = parent.parent) {
      if (excluded.has(parent) || !parent.visible || parent.userData?.dynamic) return false;
    }
    return !excluded.has(group);
  };
  group.traverse(object => {
    if (object.isMesh) stats.before++;
    if (!object.isMesh || object.isInstancedMesh || object.isSkinnedMesh ||
        object.children.length || !inheritedState(object)) return;
    const geometry = object.geometry, material = object.material;
    if (!geometry?.isBufferGeometry || !material || Array.isArray(material) ||
        material.transparent || Object.keys(geometry.morphAttributes).length ||
        geometry.drawRange.start !== 0 ||
        (Number.isFinite(geometry.drawRange.count) &&
         geometry.drawRange.count < (geometry.index?.count ?? geometry.attributes.position?.count ?? 0))) return;
    const names = Object.keys(geometry.attributes).sort();
    if (!names.includes('position') || names.some(name => geometry.attributes[name].isInstancedBufferAttribute)) return;
    const schema = names.map(name => {
      const attr = geometry.attributes[name], array = attr.isInterleavedBufferAttribute ? attr.data.array : attr.array;
      return `${name}:${attr.itemSize}:${attr.normalized}:${array.constructor.name}:${attr.gpuType}`;
    }).join('|');
    const key = `${material.uuid}|${object.castShadow}|${object.receiveShadow}|${object.renderOrder}|${object.layers.mask}|${object.frustumCulled}|${schema}`;
    if (!buckets.has(key)) buckets.set(key, []);
    buckets.get(key).push(object);
  });
  for (const members of buckets.values()) {
    if (members.length < 2) continue;
    const template = members[0], names = Object.keys(template.geometry.attributes);
    const baked = members.map(object => {
      const matrix = new THREE.Matrix4().multiplyMatrices(inverse, object.matrixWorld);
      return { geometry: object.geometry.clone().applyMatrix4(matrix), mirrored: matrix.determinant() < 0 };
    });
    const vertices = baked.reduce((sum, entry) => sum + entry.geometry.attributes.position.count, 0);
    const indexCount = baked.reduce((sum, entry) => sum + (entry.geometry.index?.count ?? entry.geometry.attributes.position.count), 0);
    const merged = new THREE.BufferGeometry();
    for (const name of names) {
      const source = baked[0].geometry.attributes[name];
      const sourceArray = source.isInterleavedBufferAttribute ? source.data.array : source.array;
      const array = new sourceArray.constructor(vertices * source.itemSize);
      let offset = 0;
      for (const { geometry } of baked) {
        const attr = geometry.attributes[name];
        if (attr.isInterleavedBufferAttribute) {
          for (let i = 0; i < attr.count; i++) for (let j = 0; j < attr.itemSize; j++) {
            array[offset++] = attr.data.array[i * attr.data.stride + attr.offset + j];
          }
        } else { array.set(attr.array, offset); offset += attr.array.length; }
      }
      const attribute = new THREE.BufferAttribute(array, source.itemSize, source.normalized);
      attribute.gpuType = source.gpuType;
      merged.setAttribute(name, attribute);
    }
    const indices = new (vertices > 65535 ? Uint32Array : Uint16Array)(indexCount);
    let indexOffset = 0, vertexOffset = 0;
    for (const { geometry, mirrored } of baked) {
      const count = geometry.index?.count ?? geometry.attributes.position.count;
      for (let i = 0; i < count; i += 3) {
        const sourceIndex = j => (geometry.index ? geometry.index.getX(j) : j) + vertexOffset;
        indices[indexOffset++] = sourceIndex(i);
        indices[indexOffset++] = sourceIndex(i + (mirrored ? 2 : 1));
        indices[indexOffset++] = sourceIndex(i + (mirrored ? 1 : 2));
      }
      if (mirrored && merged.attributes.tangent) {
        const tangent = merged.attributes.tangent;
        for (let i = 0; i < geometry.attributes.position.count; i++) {
          const index = vertexOffset + i;
          tangent.setW(index, -tangent.getW(index));
        }
      }
      vertexOffset += geometry.attributes.position.count;
      geometry.dispose(); // Only the temporary, owned geometry clone.
    }
    merged.setIndex(new THREE.BufferAttribute(indices, 1));
    merged.computeBoundingBox(); merged.computeBoundingSphere();
    const batch = new THREE.Mesh(merged, template.material);
    batch.name = `${group.name || 'scene'}_static_batch_${stats.batches}`;
    batch.castShadow = template.castShadow;
    batch.receiveShadow = template.receiveShadow;
    batch.renderOrder = template.renderOrder;
    batch.layers.mask = template.layers.mask;
    batch.frustumCulled = template.frustumCulled;
    for (const object of members) object.removeFromParent();
    group.add(batch);
    stats.mergedMeshes += members.length;
    stats.batches++;
  }
  group.traverse(object => { if (object.isMesh) stats.after++; });
  return stats;
}
