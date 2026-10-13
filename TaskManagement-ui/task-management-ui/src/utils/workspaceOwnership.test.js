import { test } from 'node:test';
import { strict as assert } from 'node:assert';
import { isWorkspaceOwner } from './workspaceOwnership.js';

test('only the workspace owner receives the ownership indicator', () => {
  const workspace = { owner: { id: 7 } };
  assert.equal(isWorkspaceOwner(workspace, { id: '7' }), true);
  assert.equal(isWorkspaceOwner(workspace, { id: 8, role: 'ADMIN' }), false);
  assert.equal(isWorkspaceOwner(workspace, null), false);
  assert.equal(isWorkspaceOwner({ owner: null }, { id: 7 }), false);
});
