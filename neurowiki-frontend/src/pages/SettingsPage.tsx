import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { authService } from '../services/authService';
import Card from '../components/Card';
import { User as UserIcon, Mail, Lock, CheckCircle2, AlertCircle, Save } from 'lucide-react';

export const SettingsPage: React.FC = () => {
  const { user, updateUser } = useAuth();

  // Profile Form State
  const [username, setUsername] = useState(user?.username || '');
  const [email, setEmail] = useState(user?.email || '');
  const [profileMsg, setProfileMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [isUpdatingProfile, setIsUpdatingProfile] = useState(false);

  // Password Form State
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordMsg, setPasswordMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [isUpdatingPassword, setIsUpdatingPassword] = useState(false);

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setProfileMsg(null);

    if (!username.trim() || !email.trim()) {
      setProfileMsg({ type: 'error', text: 'Username and email cannot be empty' });
      return;
    }

    setIsUpdatingProfile(true);
    try {
      const res = await authService.updateProfile({ username, email });
      if (res.ok && res.data) {
        updateUser(res.data);
        setProfileMsg({ type: 'success', text: 'Profile updated successfully!' });
      } else {
        setProfileMsg({ type: 'error', text: res.message || 'Failed to update profile' });
      }
    } catch (err: any) {
      setProfileMsg({ type: 'error', text: err?.message || 'Error updating profile' });
    } finally {
      setIsUpdatingProfile(false);
    }
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordMsg(null);

    if (!currentPassword || !newPassword) {
      setPasswordMsg({ type: 'error', text: 'Current and new password are required' });
      return;
    }

    if (newPassword.length < 6) {
      setPasswordMsg({ type: 'error', text: 'New password must be at least 6 characters' });
      return;
    }

    if (newPassword !== confirmPassword) {
      setPasswordMsg({ type: 'error', text: 'New passwords do not match' });
      return;
    }

    setIsUpdatingPassword(true);
    try {
      const res = await authService.changePassword({ currentPassword, newPassword });
      if (res.ok) {
        setPasswordMsg({ type: 'success', text: 'Password changed successfully!' });
        setCurrentPassword('');
        setNewPassword('');
        setConfirmPassword('');
      } else {
        setPasswordMsg({ type: 'error', text: res.message || 'Failed to change password' });
      }
    } catch (err: any) {
      setPasswordMsg({ type: 'error', text: err?.message || 'Error changing password' });
    } finally {
      setIsUpdatingPassword(false);
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Account Settings</h1>
          <p className="page-subtitle">Manage your profile information and security settings</p>
        </div>
      </div>

      <div className="settings-grid">
        <Card className="settings-card">
          <div className="card-header-with-icon">
            <UserIcon size={20} className="card-icon" />
            <h3>Profile Settings</h3>
          </div>

          {profileMsg && (
            <div className={`alert alert-${profileMsg.type} margin-bottom-md`}>
              {profileMsg.type === 'success' ? <CheckCircle2 size={16} /> : <AlertCircle size={16} />}
              <span>{profileMsg.text}</span>
            </div>
          )}

          <form onSubmit={handleUpdateProfile}>
            <div className="form-group">
              <label className="form-label">Username</label>
              <div className="input-with-icon">
                <UserIcon size={18} className="input-icon" />
                <input
                  type="text"
                  className="form-input"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Email Address</label>
              <div className="input-with-icon">
                <Mail size={18} className="input-icon" />
                <input
                  type="email"
                  className="form-input"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </div>
            </div>

            <button type="submit" className="btn btn-primary" disabled={isUpdatingProfile}>
              <Save size={16} /> {isUpdatingProfile ? 'Saving...' : 'Update Profile'}
            </button>
          </form>
        </Card>

        <Card className="settings-card">
          <div className="card-header-with-icon">
            <Lock size={20} className="card-icon" />
            <h3>Change Password</h3>
          </div>

          {passwordMsg && (
            <div className={`alert alert-${passwordMsg.type} margin-bottom-md`}>
              {passwordMsg.type === 'success' ? <CheckCircle2 size={16} /> : <AlertCircle size={16} />}
              <span>{passwordMsg.text}</span>
            </div>
          )}

          <form onSubmit={handleChangePassword}>
            <div className="form-group">
              <label className="form-label">Current Password</label>
              <div className="input-with-icon">
                <Lock size={18} className="input-icon" />
                <input
                  type="password"
                  className="form-input"
                  placeholder="Enter current password"
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">New Password</label>
              <div className="input-with-icon">
                <Lock size={18} className="input-icon" />
                <input
                  type="password"
                  className="form-input"
                  placeholder="Minimum 6 characters"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Confirm New Password</label>
              <div className="input-with-icon">
                <Lock size={18} className="input-icon" />
                <input
                  type="password"
                  className="form-input"
                  placeholder="Repeat new password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                />
              </div>
            </div>

            <button type="submit" className="btn btn-primary" disabled={isUpdatingPassword}>
              <Save size={16} /> {isUpdatingPassword ? 'Updating Password...' : 'Change Password'}
            </button>
          </form>
        </Card>
      </div>
    </div>
  );
};

export default SettingsPage;
