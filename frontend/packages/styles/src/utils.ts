/**
 * 样式工具函数
 */

/**
 * 生成响应式类名
 */
export function responsiveClass(baseClass: string, breakpoint?: 'sm' | 'md' | 'lg' | 'xl'): string {
  if (!breakpoint) return baseClass;
  return `${baseClass}-${breakpoint}`;
}

/**
 * 生成颜色工具类
 */
export function colorClass(type: 'text' | 'bg' | 'border', color: string, variant?: string): string {
  const variantSuffix = variant ? `-${variant}` : '';
  return `${type}-${color}${variantSuffix}`;
}

/**
 * 动态计算字体大小
 */
export function fontSize(size: number, unit: 'px' | 'rem' | 'em' = 'rem'): string {
  const baseSize = 16; // 16px = 1rem
  
  switch (unit) {
    case 'px':
      return `${size}px`;
    case 'rem':
      return `${size / baseSize}rem`;
    case 'em':
      return `${size / baseSize}em`;
    default:
      return `${size}px`;
  }
}

/**
 * 生成间距工具类
 */
export function spacing(type: 'm' | 'p', direction: 't' | 'r' | 'b' | 'l' | 'x' | 'y' | '', size: number): string {
  const prefix = type === 'm' ? 'margin' : 'padding';
  let directionSuffix = '';
  
  switch (direction) {
    case 't':
      directionSuffix = '-top';
      break;
    case 'r':
      directionSuffix = '-right';
      break;
    case 'b':
      directionSuffix = '-bottom';
      break;
    case 'l':
      directionSuffix = '-left';
      break;
    case 'x':
      return `${prefix}-left: ${size}px; ${prefix}-right: ${size}px;`;
    case 'y':
      return `${prefix}-top: ${size}px; ${prefix}-bottom: ${size}px;`;
    default:
      return `${prefix}: ${size}px;`;
  }
  
  return `${prefix}${directionSuffix}: ${size}px;`;
}

/**
 * 生成阴影样式
 */
export function boxShadow(
  level: 0 | 1 | 2 | 3 | 4 = 1,
  color: string = 'rgba(0, 0, 0, 0.1)',
  inset: boolean = false
): string {
  const shadows = [
    'none',
    `0 2px 4px ${color}`,
    `0 4px 8px ${color}`,
    `0 8px 16px ${color}`,
    `0 16px 32px ${color}`
  ];
  
  const shadow = shadows[level];
  return inset ? `inset ${shadow}` : shadow;
}

/**
 * 生成圆角样式
 */
export function borderRadius(
  radius: string = '4px',
  positions?: ('top-left' | 'top-right' | 'bottom-right' | 'bottom-left')[]
): string {
  if (!positions || positions.length === 0) {
    return `border-radius: ${radius};`;
  }
  
  const cssProperties = positions.map(pos => {
    const property = pos
      .split('-')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join('');
    return `border${property}Radius: ${radius};`;
  });
  
  return cssProperties.join(' ');
}

/**
 * 生成过渡动画
 */
export function transition(
  properties: string[] = ['all'],
  duration: number = 0.3,
  timing: string = 'ease-in-out',
  delay: number = 0
): string {
  const transitionValue = properties
    .map(prop => `${prop} ${duration}s ${timing} ${delay}s`)
    .join(', ');
    
  return `transition: ${transitionValue};`;
}