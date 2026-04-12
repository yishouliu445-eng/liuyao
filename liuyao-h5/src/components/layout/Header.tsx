import { Link, useLocation } from 'react-router-dom';

export default function Header() {
  const location = useLocation();
  const onSessionPage = location.pathname.startsWith('/session');
  return (
    <header className="site-header">
      <div className="header-inner">
        <Link to="/" className="header-title">
          六爻
        </Link>
        <nav className="header-nav">
          <Link to="/" className={`nav-link ${location.pathname === '/' ? 'active' : ''}`}>
            起卦
          </Link>
          <Link to="/cases" className={`nav-link ${location.pathname.startsWith('/cases') ? 'active' : ''}`}>
            案例
          </Link>
          {onSessionPage && <span className="nav-link active nav-link-static">对话中</span>}
        </nav>
      </div>
    </header>
  );
}
