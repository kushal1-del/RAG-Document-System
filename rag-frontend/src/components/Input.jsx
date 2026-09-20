function Input({
  label,
  type = "text",
  value,
  onChange,
  placeholder,
  required,
}) {
  return (
    <div className="mb-4">
      <label className="block text-zinc-500 dark:text-zinc-400 text-xs font-semibold uppercase tracking-wide mb-2">
        {label}
      </label>
      <input
        type={type}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        required={required}
        className="w-full px-3.5 py-3 rounded-xl bg-white dark:bg-zinc-800
          border border-zinc-200 dark:border-zinc-700 text-zinc-900 dark:text-white
          placeholder:text-zinc-400 outline-none
          focus:border-violet-500 focus:ring-4 focus:ring-violet-500/10
          transition-all duration-150"
      />
    </div>
  );
}

export default Input;
