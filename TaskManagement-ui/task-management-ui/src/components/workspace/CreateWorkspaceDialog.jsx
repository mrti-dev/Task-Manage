import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
} from '@mui/material';
import { createWorkspaceApi } from '../../api/workspaceApi';
import { emit } from '../../utils/eventBus';

const EMPTY_FORM = { name: '', description: '' };

export default function CreateWorkspaceDialog({ open, onClose, onCreated }) {
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const close = () => {
    if (loading) return;
    setForm(EMPTY_FORM);
    setError('');
    onClose();
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const name = form.name.trim();
    if (!name) {
      setError('Workspace name is required');
      return;
    }

    setLoading(true);
    setError('');
    try {
      const { data: response } = await createWorkspaceApi({
        name,
        description: form.description.trim(),
      });
      const workspace = response?.data ?? response;
      emit('workspace:created', workspace);
      setForm(EMPTY_FORM);
      onClose();
      onCreated?.(workspace);
    } catch (requestError) {
      setError(requestError.response?.data?.message || requestError.message || 'Failed to create workspace');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog
      open={open}
      onClose={close}
      maxWidth="sm"
      fullWidth
      PaperProps={{ sx: { borderRadius: 4, p: 1 } }}
    >
      <Box component="form" onSubmit={handleSubmit}>
        <DialogTitle sx={{ fontWeight: 700, color: '#17201F' }}>Create Workspace</DialogTitle>
        <DialogContent>
          {error && <Alert severity="error" sx={{ mb: 2, borderRadius: 2 }}>{error}</Alert>}
          <TextField
            autoFocus
            fullWidth
            required
            label="Workspace Name"
            value={form.name}
            onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
            sx={{ mt: 1, mb: 2 }}
          />
          <TextField
            fullWidth
            multiline
            rows={3}
            label="Description"
            value={form.description}
            onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={close} disabled={loading} sx={{ color: '#66736F' }}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={loading} sx={{ borderRadius: 2, bgcolor: '#0F766E' }}>
            {loading ? 'Creating...' : 'Create'}
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}
