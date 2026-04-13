import { Link, useLocation } from 'react-router-dom';

const items = [
  { to: '/', label: '起卦' },
  { to: '/history', label: '历史' },
  { to: '/calendar', label: '日历' },
  { to: '/cases', label: '案例' },
];

export default function BottomNav() {
  const location = useLocation();

  return (
    <nav className="bottom-nav" aria-label="底部导航">
      <div className="bottom-nav-inner">
        {items.map((item) => {
          const active = item.to === '/'
            ? location.pathname === '/'
            : location.pathname.startsWith(item.to);

          return (
            <Link
              key={item.to}
              to={item.to}
              className={`bottom-nav-link ${active ? 'active' : ''}`}
            >
              {item.label}
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
